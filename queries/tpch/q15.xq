<results> {
 for $customer in doc('customers.xml')/Customers/Customer
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $nation in doc('nations.xml')/Nations/Nation
 let $nname := $nation/Name
 let $region := $nation/RegionKey
 let $sname := $supplier/Name
 let $sacctbal := $supplier/AcctBal
 let $cname := $customer/Name
 let $cacctbal := $customer/AcctBal
 where $supplier/NationKey = $nation/@key and
       $customer/NationKey = $nation/@key and
       $customer/MktSegment != "BUILDING"
 group by $region, $nname, $sname
 order by sum($cacctbal/VP), count($customer/VP) descending, $nname/VP, $sname/VP
 return
 <record>
     <n_name>{$nname}</n_name>
     <s_name>{$sname}</s_name>
     <r_region>{$region}</r_region>
     <sum_sbalance>{sum($sacctbal)}</sum_sbalance>
     <count_supplier>{count($supplier)}</count_supplier>
     <avg_sbalance>{avg($sacctbal)}</avg_sbalance>
     <sum_cbalance>{sum($cacctbal)}</sum_cbalance>
     <count_customer>{count($customer)}</count_customer>
     <avg_cbalance>{avg($cacctbal)}</avg_cbalance>
  </record>  
} </results>