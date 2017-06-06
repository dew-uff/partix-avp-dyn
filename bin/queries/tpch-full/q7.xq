<results> {
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $order in doc('orders.xml')/Orders/Order
 for $nation in doc('nations.xml')/Nations/Nation
 let $region := $nation/RegionKey
 let $nname := $nation/Name
 let $sacctbal := $supplier/AcctBal
 where $supplier/NationKey = $nation/@key and
       $supplier/@Key = $order/LineItem/SuppKey
 group by $region, $nname
 order by $region/VP
 return
 <record>
     <avg_balance>{avg($sacctbal)}</avg_balance>
     <n_name>{$nname}</n_name>
     <r_region>{$region}</r_region>
  </record>
} </results>