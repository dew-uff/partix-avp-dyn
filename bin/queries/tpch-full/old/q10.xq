<results> {
 for $customer in doc('customers.xml')/Customers/Customer
 for $order in doc('orders.xml')/Orders/Order
 for $nation in doc('nations.xml')/Nations/Nation
 let $region := $nation/RegionKey
 let $nname := $nation/Name
 where $customer/@key = $order/CustKey and
       $customer/NationKey = $nation/@key
 group by $region, $nname
 order by $region/VP, avg($customer/AcctBal/VP) descending
 return
 <record>
      <avg_balance>{avg($customer/AcctBal)}</avg_balance>
      <n_name>{$nname}</n_name>
  </record>
} </results>