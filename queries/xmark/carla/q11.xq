<results>
{
 for $it in doc('xmark.xml')/site/regions/africa/item
 for $co in doc('xmark.xml')/site/closed_auctions/closed_auction
 where $co/itemref/@item = $it/@id
 and $it/payment = "Cash"
 return
 <record>
 <itens>
{$co/price}
{$co/date}
{$co/quantity}
{$co/type}
{$it/payment}
{$it/location}
{$it/from}
{$it/to}
 </itens>
 </record>
 } </results>