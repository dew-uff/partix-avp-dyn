<results> {
 let $lineitem := doc('orders.xml')/Orders/Order/LineItem[ShipDate >= "1994-01-01" and ShipDate < "1995-01-01" and
 Discount >= 0.0500 and Discount <= 0.0799 and Quantity < 24]
 return
 <record>
     <sum_revenue>{sum($lineitem/Revenue)}</sum_revenue>
  </record>
} </results>
