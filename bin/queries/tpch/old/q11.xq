<results> {
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $order in doc('orders.xml')/Orders/Order
 for $nation in doc('nations.xml')/Nations/Nation
 let $region := $nation/RegionKey
 let $nname := $nation/Name
 where $supplier/@Key = $order/LineItem/SuppKey and
       $supplier/NationKey = $nation/@key
 group by $region, $nname
 order by $region/VP, avg($supplier/AcctBal/VP) descending
 return
 <record>
     <avg_balance>{avg($supplier/AcctBal)}</avg_balance>
     <nation>{$nation/Name}</nation>
  </record>
} </results>