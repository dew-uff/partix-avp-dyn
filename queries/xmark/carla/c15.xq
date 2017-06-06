<results>
{
for $it in collection('asiaItens')/orders/itens/item
 for $pe in collection('asiaItens')/orders/people/person
 where $pe/profile/interest/@category =
$it/incategory/@category
 return
<people>
{$pe}
</people>
} </results>