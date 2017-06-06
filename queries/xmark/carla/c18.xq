<results>{
for $pe in collection('samericaItens')/orders/people/person
 let $wt := $pe//watch
 where count($wt) > 1
 return
<people>
{$pe/name}
{$pe/emailaddress}
{$pe/address}
{$pe/profile}
{$pe/watches}
</people>
}</results>