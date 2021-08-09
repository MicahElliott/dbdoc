#! /bin/zsh

# Convert a dbdoc.org file to html and send to basic auth'd confluence
# https://developer.atlassian.com/server/confluence/confluence-rest-api-examples/#update-a-page

user=${CONFLUENCE_USER?provide confluence username}
pswd=${CONFLUENCE_PASSWORD?provide confluence basic auth password}
endpoint=${CONFLUENCE_ENDPOINT?provide confluence URL endpooint}

print "Generating HTML from dbdoc.org"
html=$(pandoc -s $docs/dbdoc.org)

print "Sending dbdoc generated HTML to Confluence page DBDoc"
curl -u ${user}:${pswsd} -X PUT -H 'Content-Type: application/json' \
     -d '{"id":"3604482","type":"page", "title":"DBDoc","space":{"key":"TST"},"body":{"storage":{"value": '"$html"', "representation":"storage"}}, "version":{"number":2}}' \
     $endpoint
# http://localhost:8080/confluence/rest/api/content/3604482
