class UrlMappings {

	static mappings = {

//		"/"(controller:'login', action:'/auth')
//		"/"(controller:'userInfo', action:'/edit')
		
//		"/"(controller:'home', action:"/index")

		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"500"(view:'/error')
	}
}
