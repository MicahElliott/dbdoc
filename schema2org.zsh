#! /bin/zsh

# Convert a schema dump from SQL DDL to a seed of ORG for filling out docs

db=${DBDOC_DB?provide DB name as seed}

pg_dump -s $db |
    sed -e '/^CREATE TABLE public\.[a-z_]* (/,/^)\;/!d' -e 's/CREATE TABLE public\.//' |
    gsed -r -e 's/^    ([a-z_]+) .*/- \1 ::/' -e 's/ \($//' -e 's/\);//' -e 's/^([a-z]+)/* \1/'
