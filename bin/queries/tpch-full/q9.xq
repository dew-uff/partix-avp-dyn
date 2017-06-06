<results> {
 for $part in doc('parts.xml')/Parts/Part
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $nation in doc('nations.xml')/Nations/Nation
 for $region in doc('regions.xml')/Regions/Region
 let $rname := $region/Name
 let $supplycost := $part/PartSupp/SupplyCost
 where $supplier/NationKey = $nation/@key and
       $nation/RegionKey = $region/@key and
       $part/PartSupp/Suppkey = $supplier/@Key
 group by $rname
 order by $rname/VP
 return
 <record>  
     <avg_supcost>{avg($supplycost)}</avg_supcost>
     <n_nations>{$rname}</n_nations>
  </record>
} </results>