<results> {
 for $order in doc('orders.xml')/Orders/Order
 return
 <record>
     <avg_tax>{avg($order/LineItem/Tax)}</avg_tax>
     </record>
} </results>