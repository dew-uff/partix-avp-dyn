<results> {
 for $order in doc('orders.xml')/Orders/Order
 for $customer in doc('customers.xml')/Customers/Customer
 let $orderkey := $order/OrderKey
 let $orderdate := $order/OrderDate
 let $totalprice := $order/TotalPrice
 let $lineitem := $order/LineItem
 let $cacctbal := $customer/AcctBal
 let $extendedprice := $lineitem/ExtendedPrice
 let $discountprice := $lineitem/DiscountPrice
 let $discount := $lineitem/Discount
 let $charge := $lineitem/Charge
 let $ckey := $customer/@key
 let $cname := $customer/Name
 let $quantity := $lineitem/Quantity
 where sum($quantity) > 100
 and $order/CustKey = $customer/@key
 group by $cname, $ckey, $orderkey, $orderdate, $totalprice
 order by sum($charge/VP) descending, $totalprice/VP descending, $orderdate/VP
 return
  <record>
  <c_name>{$cname}</c_name>
  <c_key>{$ckey}</c_key>
  <o_orderkey>{$orderkey}</o_orderkey>
  <o_orderdate>{$orderdate}</o_orderdate>
  <o_totalprice>{$totalprice}</o_totalprice>
  <sum_quantity>{sum($quantity)}</sum_quantity>
  <count_lineitem>{count($lineitem)}</count_lineitem>
  <avg_quantity>{avg($quantity)}</avg_quantity>
  <sum_cbalance>{sum($cacctbal)}</sum_cbalance>
  <sum_base_price>{sum($extendedprice)}</sum_base_price>
  <sum_disc_price>{sum($discountprice)}</sum_disc_price>
  <sum_disc>{sum($discount)}</sum_disc>
  <sum_charge>{sum($charge)}</sum_charge>
  <avg_price>{avg($extendedprice)}</avg_price>
  <avg_disc_price>{avg($discount)}</avg_disc_price>
  <avg_disc>{avg($discount)}</avg_disc>
  <avg_charge>{avg($charge)}</avg_charge>
  </record>
} </results>