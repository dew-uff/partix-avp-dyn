<results>
{
for $it in collection('samericaItens')/orders/itens/item
 for $pe in collection('samericaItens')/orders/people/person
 where $pe/profile/interest/@category =
$it/incategory/@category
 return
<people>
{$pe}
</people>
} </results>