<results> {
 for $customer in doc('customers.xml')/Customers/Customer
 for $order in doc('orders.xml')/Orders/Order
 for $nation in doc('nations.xml')/Nations/Nation
 for $lineitem in $order/LineItem
 let $discountprice := $lineitem/DiscountPrice
 let $custkey := $customer/@key
 let $cname := $customer/Name
 let $cacctbal := $customer/AcctBal
 let $cphone := $customer/Phone
 let $caddress := $customer/Address
 let $ccomment := $customer/Comment
 let $nname := $nation/Name
 where $customer/@key = $order/CustKey and
       $order/OrderDate >= "1993-10-01" and
       $order/OrderDate < "1994-01-01" and
       $lineitem/ReturnFlag = "R" and
       $customer/NationKey = $nation/@key
 group by $custkey, $cname, $cacctbal, $cphone, $nname, $caddress, $ccomment 
 order by sum($discountprice/VP) descending
 return
 <record>
     <c_custkey>{$custkey}</c_custkey>
     <c_name>{$cname}</c_name>
     <sum_revenue>{sum($discountprice)}</sum_revenue>
     <c_acctbal>{$cacctbal}</c_acctbal>
     <n_name>{$nname}</n_name>
     <c_address>{$caddress}</c_address>
     <c_phone>{$cphone}</c_phone>
     <c_comment>{$ccomment}</c_comment>
  </record>
} </results>