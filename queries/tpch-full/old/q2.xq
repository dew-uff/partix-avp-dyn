<results> {
 for $order in doc('orders.xml')/Orders/Order
 for $customer in doc('customers.xml')/Customers/Customer
 for $lineitem in $order/LineItem
 let $orderkey := $order/OrderKey
 let $orderdate := $order/OrderDate
 let $shippriority := $order/ShipPriority
 let $discountprice := $lineitem/DiscountPrice
 where $customer/MktSegment = "BUILDING" and
       $customer/@key = $order/CustKey and
       $order/OrderDate < "1995-03-15" and
       $lineitem/ShipDate > "1995-03-15"
 group by $orderkey, $orderdate, $shippriority
 order by sum($discountprice/VP) descending, $orderdate/VP
 return
 <record>
    <l_orderkey>{$order/OrderKey}</l_orderkey>
    <sum_revenue>{sum($discountprice)}</sum_revenue>
    <o_orderdate>{$order/OrderDate}</o_orderdate>
    <o_shippriority>{$order/ShipPriority}</o_shippriority>
 </record>
} </results>