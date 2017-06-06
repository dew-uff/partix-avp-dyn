<results> {
 for $order in doc('orders.xml')/Orders/Order
 for $customer in doc('customers.xml')/Customers/Customer
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $nation in doc('nations.xml')/Nations/Nation
 for $region in doc('regions.xml')/Regions/Region
 let $nname := $nation/Name
 let $discountprice := $order/LineItem/DiscountPrice
 where $order/LineItem/SuppKey = $supplier/@Key and
  $customer/NationKey = $supplier/NationKey and
  $supplier/NationKey = $nation/@key and
  $nation/RegionKey = $region/@key and
  $customer/@key = $order/CustKey and
  $region/Name = "ASIA" and
  $order/OrderDate >= "1994-01-01" and
  $order/OrderDate < "1995-01-01"
  group by $nname
  order by sum($discountprice/VP) descending
 return
 <record>
    <n_name>{$nname}</n_name>
    <sum_disc_price>{sum($discountprice)}</sum_disc_price>
 </record>
} </results>