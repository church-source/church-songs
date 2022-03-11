-- churchsongs schema

-- !Ups
create table if not exists artists
(
    id bigint auto_increment
        primary key,
    name varchar(1000) not null,
    constraint artists_name_uindex
        unique (name)
);

create table if not exists songs
(
    id bigint auto_increment
        primary key,
    code varchar(100) not null,
    name varchar(1000) null,
    secondary_name varchar(1000) null,
    song_key varchar(10) null,
    artist bigint null,
    style varchar(1000) null,
    tempo varchar(10) null,
    ccli_number varchar(100) null,
    video_link varchar(2500) null,
    piano_sheet varchar(1000) null,
    lead_sheet varchar(1000) null,
    guitar_sheet varchar(1000) null,
    lyrics_sheet varchar(1000) null,
    lyrics_text text null,
    constraint songs_code_uindex
        unique (code),
    constraint songs_artists_id_fk
        foreign key (artist) references artists (id)
);

ALTER TABLE songs ADD FULLTEXT (name, secondary_name);
ALTER TABLE songs ADD FULLTEXT (lyrics_text);

# --- !Downs