<results>
{
 for $p in doc('xmark.xml')/site/people/person
 let $e := $p/homepage
 where count($e) = 0
 return
 <record>
    <people_without_homepage>
    {$p/name}
    </people_without_homepage>
 </record>
} </results>