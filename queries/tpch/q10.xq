<results> {
 for $lineitem in doc('orders.xml')/Orders/Order/LineItem
 let $tax := $lineitem/Tax
 let $shipmode := $lineitem/ShipMode
 group by $shipmode
 order by $shipmode/VP
 return
 <record>
    <shipmode>{$shipmode}</shipmode>
    <avg_tax>{avg($tax)}</avg_tax>
 </record>
} </results>