# Captain git-hook manager control file

pre_commit=(
    'hithere: echo  ## just print changed files'
    # mdlint
    cljfmt
    'cljlint(dbdoc)' # won't work since no longer a .clj file
    fixcheck
    wscheck
)

commit_msg=( msglint )

prepare_commit_msg=( br2msg )

post_commit=( colorquote )
