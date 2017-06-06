<results> {
 for $part in doc('parts.xml')/Parts/Part
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $nation in doc('nations.xml')/Nations/Nation
 for $region in doc('regions.xml')/Regions/Region
 let $rname := $region/Name
 where $part/PartSupp/Suppkey = $supplier/@Key and
       $supplier/NationKey = $nation/@key and
       $nation/RegionKey = $region/@key
 group by $rname
 order by avg($part/PartSupp/SupplyCost/VP) descending, $rname/VP
 return
 <record>
     <avg_supcost>{avg($part/PartSupp/SupplyCost)}</avg_supcost>
     <n_nations>{$region/Name}</n_nations>
  </record>
} </results>