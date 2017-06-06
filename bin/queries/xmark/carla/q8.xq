<results>
{
 for $pe in doc('xmark.xml')/site/people/person
 let $int := $pe/profile/interest
 where $pe/profile/business = "Yes"
 and count($int) > 1
 order by $pe/name/VP
 return
 <record>
   <people>{$pe}</people>
 </record>
 } </results>