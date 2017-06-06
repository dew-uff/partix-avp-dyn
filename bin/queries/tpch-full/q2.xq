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
 order by $orderdate/VP, sum($discountprice/VP) descending 
 return
 <record>
    <l_orderkey>{$orderkey}</l_orderkey>
    <sum_disc_price>{sum($discountprice)}</sum_disc_price>
    <o_orderdate>{$orderdate}</o_orderdate>
    <o_shippriority>{$shippriority}</o_shippriority>
 </record>
} </results>