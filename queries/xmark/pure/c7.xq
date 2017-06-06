let $auction := doc("xmark") return
for $p in $auction/site
return
  count($p//description) + count($p//annotation) + count($p//emailaddress)
