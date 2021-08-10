#! /bin/zsh

# Convert a schema dump from SQL DDL to a seed of ORG for filling out docs

db=${DBDOC_DB?provide DB name as seed}

print "#+Title: $DBDOC_DB\n"

print \
"This is the dbdoc description file for the database. See the
[[https://github.com/micahelliott/dbdoc][dbdoc README]]
for more detailed instructions on its purpose and expanding it. This file
contains short documentation for any tables and columns that could use
even the slightest bit of explanation.

Edit this file whenever you make schema changes. And be a good citizen
by helping to grow this file any time you're touching a table!
The remainder of this file will be used processed into comment
descriptions that will be visible in your SQL client, and can also be
exported as HTML.
"

pg_dump -s $db |
    sed -e '/^CREATE TABLE public\.[a-z_]* (/,/^)\;/!d' -e 's/CREATE TABLE public\.//' |
    gsed -r -e 's/^    ([a-z_]+) .*/- \1 ::/' -e 's/ \($//' -e 's/\);//' -e 's/^([a-z]+)/* \1/'
