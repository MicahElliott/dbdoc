# Captain git-hook manager control file

pre_commit=(
    'hithere: echo  ## just print changed files'
    mdlint
    fixcheck
    wscheck
)

commit_msg=( msglint )

prepare_commit_msg=( br2msg )

post_commit=( colorquote )
