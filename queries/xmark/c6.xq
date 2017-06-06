<results> {
let $auction := doc("xmark100m.xml") return
 for $b in $auction//site/regions return <total_itens> { count($b//item) } </total_itens>
} </results>
