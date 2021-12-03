# See frequencies for table usage in TSV

# Example:
# ...
# 15      rate_sheet_entry
# 16      project_root_doc_pkgs
# 24      rate_sheet
# 28      loan_type
# 36      capitalprovider
# 50      project_root


nqueries() { grep -EiI "(from|join) +.*\b${1}\b" *.sql | wc -l }
files()    { grep -EiI "(from|join) +.*\b${1}\b" *.sql | cut -d: -f1 | sort -n | uniq }

cd resources/sql
grep -E '^\* ' ../../docs/dbdoc.org |
    gsed -r -e 's/^\* //' -e 's/-/_/g' |
    while read -r table; do print "$(nqueries $table)\t$table"; done |
    sort -n
