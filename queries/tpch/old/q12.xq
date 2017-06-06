<results> {
 let $part := doc('parts.xml')/Parts/Part
 return
 <record>
     <avg_size>{avg($part/Size)}</avg_size>
  </record>
} </results>