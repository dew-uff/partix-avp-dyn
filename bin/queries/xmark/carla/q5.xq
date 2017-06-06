<results>
{
 for $it in doc('xmark.xml')/site/regions/asia/item
 for $pe in doc('xmark.xml')/site/people/person
 where $pe/profile/interest/@category = $it/incategory/@category
 return
 <record>
    <people>{$pe}</people>
 </record>
 } </results>