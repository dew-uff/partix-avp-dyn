<results>
 {
 for $c in doc('dblp.xml')/dblp/inproceedings
 where $c/year > 1984 and $c/year <= 2007
 return
 <inproceeding>
 { $c/title }
 </inproceeding>
 } </results>