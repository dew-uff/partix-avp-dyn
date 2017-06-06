<results>
{
 for $p in doc('xmark.xml')/site/people/person
 where $p/profile/@income > 10000
 return
 <record>
    <people>{$p}</people>
 </record>
} </results>