<results>{
for $pe in collection('asiaItens')/orders/people/person
 let $wt := $pe//watch
 where count($wt) > 1
 return
<people>{$pe}</people>
}</results>