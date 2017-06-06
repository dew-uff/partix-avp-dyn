<results> {
 for $part in doc('parts.xml')/Parts/Part
 let $partsize := $part/Size
 let $container := $part/Container
 group by $container
 order by avg($partsize/VP) descending
 return
 <record>
     <container>{$container}</container>
     <p_size>{$partsize}</p_size>
     <avg_size>{avg($partsize)}</avg_size>
  </record>
} </results>