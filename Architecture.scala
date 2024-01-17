object Architecture {
  val routes = Routes()
    .handleGet("/hello") { req =>
      IO.delayR(Response(200, "Hello"))
    }
    .handleGet("/proxy") { req =>
      val address = req.body
      val response = makeRequest(address)
      println("Got response with status " + response.status)
      Response(200, response.toJson)
    }
    .handleGet("/proxyBatch") { req =>
      val addresses = req.body.split("\n")
      val responses = addresses.map(makeRequest)
      /* ^ will work as: (very simplified)

        Sequenced by: 
        def sequence[X, R](init: M[X])(f: X => Either[M[X], M[R]]): M[R] =
          Monad[M].flatten(Monad[M].tailRecM(init) { mx => Monad[M].map(mx)(f) })
        Where f is either:
           - return R
           - continuation.resume(X)

        As a result, we get more or less:

        object Context {
          val c1: Coroutine
          val c2: Coroutine
        }
        
        val r: IO[Either[DbError, Response]] = makeRequest(addresses(0))
        val either: Either[...] = suspendAndExecute(r, Context.c1) // Here, we can could instantly and not use coroutine at all
        val resp: Response = suspendAndExecute(either, Context.c2)
        
        Where suspendAndExecute(m, c) is:
          enqueue(Monad[M].map(mx)(c.resume)) // or just come back to outer context instead of enqueue, OR synchronously return the value (!)
          c.suspend()

        If CE had Loom-based execution context, then instead of suspendAndExecute we could do:
          - If scheduler decides to yield, use ReentrantLock that would poll with resume callback - transformed by hotspot (maybe something else?)
          - Otherwise, just return the value
      */

      Response(200, s"""{"responses": [${responses.map(_.toJson).mkString(",")}]}""")
    }

  
}
