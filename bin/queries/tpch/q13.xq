<results> {
 for $lineitem in doc('orders.xml')/Orders/Order/LineItem
 let $revenue := $lineitem/Revenue
 let $discount := $lineitem/Discount
 let $quantity := $lineitem/Quantity
 let $linestatus := $lineitem/LineStatus
 where $lineitem/ShipDate >= "1994-01-01" and
       $lineitem/ShipDate < "1995-01-01" and
       $lineitem/Discount >= 0.05 and
       $lineitem/Discount <= 0.07 and
       $lineitem/Quantity < 24
 group by $linestatus
 return
 <record>
    <l_linestatus>{$linestatus}</l_linestatus>
    <sum_revenue>{sum($revenue)}</sum_revenue>
 </record>
} </results>