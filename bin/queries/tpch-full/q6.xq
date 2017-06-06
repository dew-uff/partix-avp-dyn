<results> {
 for $order in doc('orders.xml')/Orders/Order
 for $customer in doc('customers.xml')/Customers/Customer
 for $nation in doc('nations.xml')/Nations/Nation
 let $region := $nation/RegionKey
 let $nname := $nation/Name
 let $cacctbal := $customer/AcctBal
 where $customer/NationKey = $nation/@key and
       $customer/@key = $order/CustKey
 group by $region, $nname
 order by $region/VP
 return
 <record>
      <avg_balance>{avg($cacctbal)}</avg_balance>
      <n_name>{$nname}</n_name>
     <r_region>{$region}</r_region>
  </record>
} </results>