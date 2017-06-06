<results>
{
 for $it in doc('xmark.xml')/site/regions/samerica/item
 for $pe in doc('xmark.xml')/site/people/person
 where $pe/profile/interest/@category = $it/incategory/@category
 and $it/incategory/@category = "category251"
 and $pe/profile/education = "Graduate School"
 return
 <record>
    <people>{$pe}</people>
 </record>
 } </results>