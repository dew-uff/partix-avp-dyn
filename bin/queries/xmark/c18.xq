<results> {
declare namespace local = "http://www.foobar.org";
 declare function local:convert($v as xs:decimal?) as xs:decimal?
{
  2.20371 * $v (: convert Dfl to Euro :)
};

 let $auction := doc("xmark100m.xml") return
 for $i in $auction/site/open_auctions/open_auction
 return <reserve_currency> { local:convert(zero-or-one($i/reserve)) } </reserve_currency>
} </results>