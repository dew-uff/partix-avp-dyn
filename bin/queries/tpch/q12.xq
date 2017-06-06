<results> {
 for $order in doc('orders.xml')/Orders/Order
 for $customer in doc('customers.xml')/Customers/Customer
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $nation in doc('nations.xml')/Nations/Nation
 let $nname := $nation/Name
 let $region := $nation/RegionKey
 let $sname := $supplier/Name
 let $sacctbal := $supplier/AcctBal
 let $cname := $customer/Name
 let $cacctbal := $customer/AcctBal
 let $extendedprice := $order/LineItem/ExtendedPrice
 let $discountprice := $order/LineItem/DiscountPrice
 let $discount := $order/LineItem/Discount
 let $charge := $order/LineItem/Charge
 let $quantity := $order/LineItem/Quantity
 where $supplier/@Key = $order/LineItem/SuppKey and
       $supplier/NationKey = $nation/@key and
       $customer/NationKey = $nation/@key and
       $customer/@key = $order/CustKey
 group by $region, $nname, $sname
 order by sum($extendedprice/VP), count($order/VP) descending, $nname/VP, $sname/VP
 return
 <record>
     <n_name>{$nname}</n_name>
     <s_name>{$sname}</s_name>
     <r_region>{$region}</r_region>
     <sum_sbalance>{sum($sacctbal)}</sum_sbalance>
     <count_supplier>{count($supplier)}</count_supplier>
     <avg_sbalance>{avg($sacctbal)}</avg_sbalance>
     <count_numwait>{count($order)}</count_numwait>
     <sum_qty>{sum($quantity)}</sum_qty>
     <sum_base_price>{sum($extendedprice)}</sum_base_price>
     <sum_disc_price>{sum($discountprice)}</sum_disc_price>
     <sum_charge>{sum($charge)}</sum_charge>
     <avg_qty>{avg($quantity)}</avg_qty>
     <avg_price>{avg($extendedprice)}</avg_price>
     <avg_disc>{avg($discount)}</avg_disc>
     <sum_cbalance>{sum($cacctbal)}</sum_cbalance>
     <count_customer>{count($customer)}</count_customer>
     <avg_cbalance>{avg($cacctbal)}</avg_cbalance>
     <c_name>{$cname}</c_name>
  </record>  
} </results>