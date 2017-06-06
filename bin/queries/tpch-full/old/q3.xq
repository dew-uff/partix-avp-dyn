<results> {
 for $order in doc('orders.xml')/Orders/Order
 let $orderpriority := $order/OrderPriority
 where $order/OrderDate >= "1993-07-01" and
       $order/OrderDate < "1993-10-01" and
       count(let $lineitem := $order/LineItem
             where $lineitem/CommitDate < $lineitem/ReceiptDate
             return $lineitem) > 0      
 group by $orderpriority
 order by $orderpriority/VP
 return
 <record>
    <o_orderpriority>{$orderpriority}</o_orderpriority>
    <count_order>{count($order)}</count_order>
 </record>
} </results>