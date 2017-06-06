let $auction := doc("xmark") return
for $b in $auction//site/regions return count($b//item)
