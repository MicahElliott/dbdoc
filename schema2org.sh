#! /bin/bash

# Convert a schema dump from SQL DDL to a seed of ORG for filling out docs

pg_dump -s cc |
    sed -e '/^CREATE TABLE public\.[a-z_]* (/,/^)\;/!d' -e 's/CREATE TABLE public\.//' |
    gsed -r -e 's/^    ([a-z_]+) .*/- \1 ::/' -e 's/ \($//' -e 's/\);//' -e 's/^([a-z]+)/* \1/'
