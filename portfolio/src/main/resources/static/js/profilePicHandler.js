const FILE_SIZE_LIM = 5000000
const MAX_IMG_DIM = 500
let vanilla


//https://github.com/josefrichter/resize/blob/master/public/preprocess.js
function processImage () {
    if (document.getElementById("userImageUpload").files.length === 0) {
    	return
    }

	const uploadedRawImage = document.getElementById("userImageUpload").files[0]
	let reader = new FileReader()
	reader.readAsArrayBuffer(uploadedRawImage)

	reader.onload = function (event) {
		let imageBlob = new Blob([event.target.result])
		let imageBlobURL = URL.createObjectURL(imageBlob)

		let image = new Image()
		image.src = imageBlobURL

		image.onload = function () {
			let canvas = document.createElement('canvas')
			let context = canvas.getContext('2d')

			let imageWidth = image.width
			let imageHeight = image.height
			
			if (imageWidth > imageHeight) {
				if (imageWidth > MAX_IMG_DIM) {
					imageHeight = Math.round (imageHeight *= MAX_IMG_DIM / imageWidth)
					imageWidth = MAX_IMG_DIM
				}
			} else {
				if (imageHeight > MAX_IMG_DIM) {
					imageWidth = Math.round (imageWidth *= MAX_IMG_DIM / imageHeight)
					imageHeight = MAX_IMG_DIM
				}
			}

			canvas.width = imageWidth
			canvas.height = imageHeight
			context.drawImage(image, 0, 0, imageWidth, imageHeight)
			canvas.toBlob(blob => {
				//set croppie source to uploaded photo and enable buttons
				let url = URL.createObjectURL(blob)
				vanilla.bind({url: url})
				document.getElementById("uploadImageButton").disabled = false
				document.getElementById("croppieImageEl").style.visibility= "visible"
			}, "image/jpeg", 0.7)
		}
	}
}


//https://www.youtube.com/watch?v=hHLxdUNn-Dg&ab_channel=CameronMcKenzie
async function uploadImage () {
	let formData = new FormData()
	formData.append("image", await vanilla.result({type: 'blob', format: 'jpeg'}))
	let response = await fetch ("api/uploadProfilePicture", {method: "POST", body: formData})

	if (response.status === 200) window.location.reload();
}
async function deleteImage () {
	let response = await fetch ("api/user/profilePicture", {method: "DELETE"});
	if (response.status === 200) window.location.reload();
}

//Initialize croppie
window.onload = function () {
	let basic = document.getElementById("croppieImageEl")
	vanilla = new Croppie(basic, {
		viewport: {width: 150, height: 150, type: 'circle'},
		showZoomer: true
	})
	basic.style.visibility = "hidden" //hide initially
}