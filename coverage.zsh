#! /bin/zsh

# TODO Annotate each missing table line with the git-blame of who (dev name) did the CREATE TABLE in migration files

db=${DBDOC_DB?provide DB name as seed}

# Array of public tables
tables=( $(rg  'CREATE TABLE public\.[A-z]+ ' $db | sed -r -e 's/CREATE TABLE public\.//' -e 's/ \(//') )
tcovered=( $(rg  'COMMENT ON TABLE public\.[A-z]+ IS' $db | sed -r -e 's/COMMENT ON TABLE public\.//' -e "s/ IS '.*//") )

# print -l $tables
# print -l $tcovered

integer ncovs=$(print -l $tcovered | wc -l)
integer ntabs=$(print -l $tables -l | wc -l)

print "### Tables missing docs ($(( ntabs - ncovs )))"

comm -23 <(print -l $tables | sort) <(print -l $tcovered | sort)

printf "\n### Coverage: %0.2f%% (%d/%d)\n"  $(( $ncovs  / $ntabs. )) $ncovs $ntabs
