package security

import com.auth0.jwk.UrlJwkProvider
import javax.inject.Inject
import pdi.jwt.{JwtAlgorithm, JwtBase64, JwtClaim, JwtJson}
import play.api.Configuration
import scala.util.{Failure, Success, Try}

class AuthService @Inject()(config: Configuration) {

  // A regex that defines the JWT pattern and allows us to
  // extract the header, claims and signature
  private val jwtRegex = """(.+?)\.(.+?)\.(.+?)""".r

  // Your Auth0 domain, read from configuration
  private def domain = config.get[String]("auth0.domain")

  // Your Auth0 audience, read from configuration
  private def audience = config.get[String]("auth0.audience")

  // The issuer of the token. For Auth0, this is just your Auth0
  // domain including the URI scheme and a trailing slash.
  private def issuer = config.get[String]("auth0.issuer")

  // Validates a JWT and potentially returns the claims if the token was
  // successfully parsed and validated
  def validateJwt(token: String, priv: String): Try[JwtClaim] = {
    for {
      //jwk <- getJwk(token)           // Get the secret key for this token
      claims <- JwtJson.decode(token, "mySecret", Seq(JwtAlgorithm.HS512)) // Decode the token using the secret key
      _ <- validateClaims(claims, priv)     // validate the data stored inside the token
    } yield claims
  }

  // Splits a JWT into it's 3 component parts
  private val splitToken = (jwt: String) => jwt match {
    case jwtRegex(header, body, sig) => Success((header, body, sig))
    case _ => Failure(new Exception("Token does not match the correct pattern"))
  }

  // As the header and claims data are base64-encoded, this function
  // decodes those elements
  private val decodeElements = (data: Try[(String, String, String)]) => data map {
    case (header, body, sig) =>
      (JwtBase64.decodeString(header), JwtBase64.decodeString(body), sig)
  }

/*  // Gets the JWK from the JWKS endpoint using the jwks-rsa library
  private val getJwk = (token: String) =>
    (splitToken andThen decodeElements) (token) flatMap {
      case (header, body, _) =>
        val jwtHeader = JwtJson.parseHeader(header)     // extract the header
        val jwkProvider = new UrlJwkProvider(s"https://$domain")

        // Use jwkProvider to load the JWKS data and return the JWK
        jwtHeader.keyId.map { k =>
          Try(jwkProvider.get(k))
        } getOrElse Success("") // TODO need to fix this Failure(new Exception("Unable to retrieve kid"))
  }
*/

  // Validates the claims inside the token. 'isValid' checks the issuedAt, expiresAt,
  // issuer and audience fields.
  private val validateClaims = (claims: JwtClaim, priv: String) =>
    if (claims.isValid(issuer, audience)) {
      if(claims.content != null && !"".equals(claims.content)){
        val pattern = "(privileges)\":\"((\\\\\"|[^\"])*){2}".r
        val text = pattern.findAllIn(claims.content).mkString
        val privClaims = text.replaceAll("privileges\":\"","").split(",")
        if(privClaims contains priv)
          Success(claims)
        else
          Failure(new Exception("User does not have sufficient privileges"))
      } else
        Failure(new Exception("User does not have sufficient privileges"))
    } else {
      Failure(new Exception("The JWT did not pass validation"))
    }

}