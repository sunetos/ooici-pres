package ooici.pres.domain

import ion.core.messaging.IonMessage
import ion.resource.ResourceDO
import com.rabbitmq.client.AMQP
import ion.core.data.DataObject
import ion.resource.InstrumentRDO

class DataproductController {

	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

	// Spring injected
	def lcademoService

	def index = {
		redirect(action: "list", params: params)
	}

	def list = {

		def allDataProducts = lcademoService.listAllDataProducts()

		def dataProducts = []
		for (d in allDataProducts) {

			def dpName = (String)d.getAttribute("name")
			if(dpName != null && dpName != '') {

				def dataProduct = new Dataproduct()

				dataProduct.name = (String) d.getAttribute("name")
				dataProduct.dataFormat = (String) d.getAttribute("dataformat")

				DataObject instrDO = (DataObject) d.getAttribute("instrument_ref")
				dataProduct.instrumentId = instrDO.getIdentity()

				dataProducts << dataProduct
			}
		}

		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		[dataProducts: dataProducts, dataproductInstanceTotal: Dataproduct.count()]
	}

	def create = {

		def allInstruments = lcademoService.listAllInstruments()

		def instruments = []
		for (i in allInstruments) {

			def instrName = (String) i.getAttribute("name").trim()
			if (instrName != null && instrName != '') {

				def instrument = new Instrument()

				instrument.name = instrName
				instrument.registryId = (String) i.getIdentity()

				instruments << instrument
			}
		}

		def dpInstance = new Dataproduct()
		dpInstance.properties = params
		return [dpInstance: dpInstance, instruments: instruments]
	}

	def save = {

		if (params.Cancel)
			redirect(controller: "home", action: "index")

		lcademoService.createDataProduct(params.name, params.instrumentId, params.dataFormat)

		def dataproductInstance = new Dataproduct(params)
		if (dataproductInstance.save(flush: true)) {
			redirect(action: "show")
		}
		else {

			def allInstruments = lcademoService.listAllInstruments()

			def instruments = []
			for (i in allInstruments) {

				def instrName = (String) i.getAttribute("name").trim()
				if (instrName != null && instrName != '') {

					def instrument = new Instrument()

					instrument.name = instrName
					instrument.registryId = (String) i.getIdentity()

					instruments << instrument
				}
			}

			render(view: "create", model: [dpInstance: dataproductInstance, instruments:instruments])
		}
	}

	def show = {
		def dataproductInstance
		if (params.id != null) {
			dataproductInstance = Dataproduct.get(params.id)
		}
		else {
			dataproductInstance = null
		}

		if (!dataproductInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'dataproduct.label', default: 'Dataproduct'), params.id])}"
			redirect(action: "list")
		}
		else {
			[dataproductInstance: dataproductInstance]
		}
	}

	def edit = {
		def dataproductInstance = Dataproduct.get(params.id)
		if (!dataproductInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'dataproduct.label', default: 'Dataproduct'), params.id])}"
			redirect(action: "list")
		}
		else {
			return [dataproductInstance: dataproductInstance]
		}
	}

	def update = {
		def dataproductInstance = Dataproduct.get(params.id)
		if (dataproductInstance) {
			if (params.version) {
				def version = params.version.toLong()
				if (dataproductInstance.version > version) {

					dataproductInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'dataproduct.label', default: 'Dataproduct')] as Object[], "Another user has updated this Dataproduct while you were editing")
					render(view: "edit", model: [dataproductInstance: dataproductInstance])
					return
				}
			}
			dataproductInstance.properties = params
			if (!dataproductInstance.hasErrors() && dataproductInstance.save(flush: true)) {
				flash.message = "${message(code: 'default.updated.message', args: [message(code: 'dataproduct.label', default: 'Dataproduct'), dataproductInstance.id])}"
				redirect(action: "show", id: dataproductInstance.id)
			}
			else {
				render(view: "edit", model: [dataproductInstance: dataproductInstance])
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'dataproduct.label', default: 'Dataproduct'), params.id])}"
			redirect(action: "list")
		}
	}

	def delete = {
		def dataproductInstance = Dataproduct.get(params.id)
		if (dataproductInstance) {
			try {
				dataproductInstance.delete(flush: true)
				flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'dataproduct.label', default: 'Dataproduct'), params.id])}"
				redirect(action: "list")
			}
			catch (org.springframework.dao.DataIntegrityViolationException e) {
				flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'dataproduct.label', default: 'Dataproduct'), params.id])}"
				redirect(action: "show", id: params.id)
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'dataproduct.label', default: 'Dataproduct'), params.id])}"
			redirect(action: "list")
		}
	}
}
