#! /bin/zsh

# https://stackoverflow.com/a/4946306/326516

lines=$( psql -t -A -F'	'  -c '
select
    c.table_schema,
    c.table_name,
    c.column_name,
    pgd.description
from pg_catalog.pg_statio_all_tables as st
inner join pg_catalog.pg_description pgd on (
    pgd.objoid = st.relid
)
inner join information_schema.columns c on (
    pgd.objsubid   = c.ordinal_position and
    c.table_schema = st.schemaname and
    c.table_name   = st.relname
);'
)

# Get db lines sorted into file
# print $lines | sort | grep '^public' | gcut -f2-4 >dbdoc-public.tsv
print $lines | sort >indb.tsv

# Get doc lines sorted into file
dbdoc.clj roundtrip 2>&1 |sort >inorg.tsv

# See just the entries unique to db
# Stupid mac really should be using gnu comm (gcomm),
comm -23 indb.tsv inorg.tsv >dbonly.tsv

# Convert TSV lines into ORG format, print newly needed entries and conflicts
tsv2org

# print '\nYou may want to delete the temp files now.'
# print '  rm indb.tsv inorg.tsv dbonly.tsv'
