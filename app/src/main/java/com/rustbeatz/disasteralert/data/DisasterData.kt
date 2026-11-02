package com.rustbeatz.disasteralert.data

data class DisasterType(
    val id: String,
    val name: String,
    val emoji: String,
    val category: String,
    val description: String
)

data class IndianState(
    val name: String,
    val code: String,
    val isUnionTerritory: Boolean = false,
    val zone: String
)

enum class AlertSeverity(
    val label: String,
    val codeName: String,
    val description: String
) {
    RED("Red Alert", "CRITICAL", "Immediate threat to life & property. Urgent evacuation ordered."),
    ORANGE("Orange Alert", "SEVERE", "High preparedness required. Responders mobilized."),
    YELLOW("Yellow Alert", "MODERATE", "Weather disruption expected. Citizens advised to remain vigilant."),
    GREEN("Green Advisory", "ADVISORY", "Monitoring phase. Relief & recovery operations underway.")
}

data class DisasterBroadcastRecord(
    val id: String,
    val disasterType: String,
    val emoji: String,
    val state: String,
    val district: String,
    val city: String,
    val severity: AlertSeverity,
    val instructions: String,
    val authorityId: String,
    val timestamp: String,
    val isDelivered: Boolean = true
)

object DisasterRepository {

    val disasterTypes = listOf(
        DisasterType("flood", "Flood / Flash Flood", "🌊", "Hydrological", "Rapid water level inundation of populated basins."),
        DisasterType("cyclone", "Cyclone / Storm Surge", "🌀", "Meteorological", "Destructive winds & coastal tidal inundation."),
        DisasterType("earthquake", "Earthquake / Tremor", "🌋", "Geological", "Subsurface seismic rupture causing structural damage."),
        DisasterType("landslide", "Landslide / Mudslide", "⛰️", "Geological", "Mass movement of rock, debris, or earth down slopes."),
        DisasterType("cloudburst", "Cloudburst / Extreme Rain", "🌩️", "Meteorological", "Sudden localized torrential rainfall causing flash torrents."),
        DisasterType("wildfire", "Forest Fire / Wildfire", "🔥", "Environmental", "Uncontrolled spread of wildland fires endangering habitats."),
        DisasterType("tsunami", "Tsunami / Sea Surge", "🌊", "Oceanic", "High energy sea waves triggered by undersea seismic activity."),
        DisasterType("chemical", "Industrial / Chemical Gas Leak", "🏭", "Technological", "Toxic vapor release or hazardous chemical spill."),
        DisasterType("avalanche", "Snow Avalanche", "❄️", "Cryospheric", "Mass descent of mountain snowpack threatening passes."),
        DisasterType("heatwave", "Severe Heat Wave", "☀️", "Climatic", "Abnormal temperatures causing heatstroke risk."),
        DisasterType("drought", "Severe Agricultural Drought", "🌾", "Hydrological", "Critical moisture deficit impacting agriculture & water."),
        DisasterType("dam_breach", "Dam Breach / Reservoir Overflow", "⚠️", "Infrastructure", "Emergency release or breach of barrage reservoir.")
    )

    val indianStates = listOf(
        IndianState("Andhra Pradesh", "AP", zone = "South"),
        IndianState("Arunachal Pradesh", "AR", zone = "North-East"),
        IndianState("Assam", "AS", zone = "North-East"),
        IndianState("Bihar", "BR", zone = "East"),
        IndianState("Chhattisgarh", "CG", zone = "Central"),
        IndianState("Goa", "GA", zone = "West"),
        IndianState("Gujarat", "GJ", zone = "West"),
        IndianState("Haryana", "HR", zone = "North"),
        IndianState("Himachal Pradesh", "HP", zone = "North"),
        IndianState("Jharkhand", "JH", zone = "East"),
        IndianState("Karnataka", "KA", zone = "South"),
        IndianState("Kerala", "KL", zone = "South"),
        IndianState("Madhya Pradesh", "MP", zone = "Central"),
        IndianState("Maharashtra", "MH", zone = "West"),
        IndianState("Manipur", "MN", zone = "North-East"),
        IndianState("Meghalaya", "ML", zone = "North-East"),
        IndianState("Mizoram", "MZ", zone = "North-East"),
        IndianState("Nagaland", "NL", zone = "North-East"),
        IndianState("Odisha", "OD", zone = "East"),
        IndianState("Punjab", "PB", zone = "North"),
        IndianState("Rajasthan", "RJ", zone = "North"),
        IndianState("Sikkim", "SK", zone = "North-East"),
        IndianState("Tamil Nadu", "TN", zone = "South"),
        IndianState("Telangana", "TG", zone = "South"),
        IndianState("Tripura", "TR", zone = "North-East"),
        IndianState("Uttar Pradesh", "UP", zone = "North"),
        IndianState("Uttarakhand", "UK", zone = "North"),
        IndianState("West Bengal", "WB", zone = "East"),
        // Union Territories
        IndianState("Andaman and Nicobar Islands", "AN", isUnionTerritory = true, zone = "Islands"),
        IndianState("Chandigarh", "CH", isUnionTerritory = true, zone = "North"),
        IndianState("Dadra and Nagar Haveli and Daman and Diu", "DD", isUnionTerritory = true, zone = "West"),
        IndianState("Delhi (NCT)", "DL", isUnionTerritory = true, zone = "North"),
        IndianState("Jammu and Kashmir", "JK", isUnionTerritory = true, zone = "North"),
        IndianState("Ladakh", "LA", isUnionTerritory = true, zone = "North"),
        IndianState("Lakshadweep", "LD", isUnionTerritory = true, zone = "Islands"),
        IndianState("Puducherry", "PY", isUnionTerritory = true, zone = "South")
    )

    val stateDistrictsMap: Map<String, List<String>> = mapOf(
        "Andhra Pradesh" to listOf("Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool", "East Godavari", "West Godavari", "Krishna", "Chittoor", "Kadapa", "Anantapur", "Srikakulam", "Vizianagaram"),
        "Arunachal Pradesh" to listOf("Itanagar", "Tawang", "West Kameng", "East Kameng", "Papum Pare", "Lower Subansiri", "Upper Subansiri", "West Siang", "East Siang", "Changlang", "Tirap"),
        "Assam" to listOf("Kamrup Metropolitan (Guwahati)", "Kamrup Rural", "Dibrugarh", "Silchar (Cachar)", "Jorhat", "Nagaon", "Tinsukia", "Sonitpur", "Barpeta", "Dhubri", "Golaghat", "Darrang", "Morigaon"),
        "Bihar" to listOf("Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Purnia", "Darbhanga", "Saran (Chhapra)", "Rohtas", "Nalanda", "Begusarai", "Samastipur", "East Champaran", "West Champaran"),
        "Chhattisgarh" to listOf("Raipur", "Bilaspur", "Durg", "Bhilai", "Korba", "Rajnandgaon", "Jagdalpur (Bastar)", "Surguja", "Raigarh", "Dhamtari", "Kanker", "Kabirdham"),
        "Goa" to listOf("North Goa (Panaji)", "South Goa (Margao)", "Ponda", "Vasco da Gama", "Mapusa"),
        "Gujarat" to listOf("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Jamnagar", "Junagadh", "Gandhinagar", "Kutch (Bhuj)", "Anand", "Bharuch", "Navsari", "Valsad", "Porbandar", "Morbi"),
        "Haryana" to listOf("Gurugram", "Faridabad", "Panipat", "Ambala", "Yamunanagar", "Rohtak", "Hisar", "Karnal", "Sonipat", "Panchkula", "Bhiwani", "Sirsa", "Rewari"),
        "Himachal Pradesh" to listOf("Shimla", "Kangra (Dharamshala)", "Mandi", "Kullu", "Solan", "Sirmaur", "Chamba", "Hamirpur", "Una", "Bilaspur", "Kinnaur", "Lahaul & Spiti"),
        "Jharkhand" to listOf("Ranchi", "Jamshedpur (East Singhbhum)", "Dhanbad", "Bokaro", "Deoghar", "Hazaribagh", "Giridih", "Ramgarh", "Palamu", "Dumka", "Chaibasa"),
        "Karnataka" to listOf("Bengaluru Urban", "Bengaluru Rural", "Mysuru", "Hubballi-Dharwad", "Mangaluru (Dakshina Kannada)", "Belagavi", "Kalaburagi", "Davanagere", "Ballari", "Shivamogga", "Tumakuru", "Udupi", "Kodagu"),
        "Kerala" to listOf("Wayanad", "Idukki", "Ernakulam (Kochi)", "Thiruvananthapuram", "Kozhikode", "Thrissur", "Malappuram", "Palakkad", "Alappuzha", "Kottayam", "Kollam", "Kannur", "Kasaragod", "Pathanamthitta"),
        "Madhya Pradesh" to listOf("Bhopal", "Indore", "Jabalpur", "Gwalior", "Ujjain", "Sagar", "Dewas", "Satna", "Ratlam", "Rewa", "Katni", "Singrauli", "Hoshangabad"),
        "Maharashtra" to listOf("Mumbai City", "Mumbai Suburban", "Pune", "Thane", "Nagpur", "Nashik", "Chhatrapati Sambhajinagar", "Solapur", "Kolhapur", "Raigad", "Ratnagiri", "Sindhudurg", "Satara", "Sangli", "Ahmednagar", "Jalgaon"),
        "Manipur" to listOf("Imphal West", "Imphal East", "Churachandpur", "Thoubal", "Bishnupur", "Senapati", "Ukhrul", "Chandel", "Tamenglong", "Kangpokpi"),
        "Meghalaya" to listOf("East Khasi Hills (Shillong)", "West Garo Hills (Tura)", "Ri-Bhoi", "West Khasi Hills", "South West Khasi Hills", "East Garo Hills", "Jaintia Hills"),
        "Mizoram" to listOf("Aizawl", "Lunglei", "Champhai", "Kolasib", "Serchhip", "Lawngtlai", "Mamit", "Saiha"),
        "Nagaland" to listOf("Kohima", "Dimapur", "Mokokchung", "Tuensang", "Wokha", "Zunheboto", "Mon", "Phek", "Chumoukedima"),
        "Odisha" to listOf("Puri", "Cuttack", "Bhubaneswar (Khordha)", "Balasore", "Ganjam", "Sambalpur", "Rourkela (Sundargarh)", "Bhadrak", "Kendrapara", "Jagatsinghpur", "Jajpur", "Mayurbhanj", "Koraput"),
        "Punjab" to listOf("Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda", "Mohali (SAS Nagar)", "Hoshiarpur", "Pathankot", "Moga", "Firozpur", "Kapurthala"),
        "Rajasthan" to listOf("Jaipur", "Jodhpur", "Kota", "Bikaner", "Ajmer", "Udaipur", "Bhilwara", "Alwar", "Sikar", "Sri Ganganagar", "Pali", "Bharatpur", "Barmer", "Jaisalmer"),
        "Sikkim" to listOf("Gangtok (East Sikkim)", "Namchi (South Sikkim)", "Mangan (North Sikkim)", "Gyalshing (West Sikkim)", "Pakyong", "Soreng"),
        "Tamil Nadu" to listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Tirunelveli", "Tiruppur", "Vellore", "Erode", "Thoothukudi", "Cuddalore", "Kanchipuram", "Kanyakumari", "Thanjavur", "Nagapattinam"),
        "Telangana" to listOf("Hyderabad", "Warangal", "Nizamabad", "Karimnagar", "Khammam", "Ramagundam", "Mahbubnagar", "Nalgonda", "Adilabad", "Suryapet", "Rangareddy", "Medchal-Malkajgiri"),
        "Tripura" to listOf("West Tripura (Agartala)", "Gomati", "South Tripura", "North Tripura", "Dhalai", "Unakoti", "Khowai", "Sepahijala"),
        "Uttar Pradesh" to listOf("Lucknow", "Kanpur", "Varanasi", "Prayagraj", "Agra", "Meerut", "Gautam Buddha Nagar (Noida)", "Ghaziabad", "Bareilly", "Aligarh", "Moradabad", "Gorakhpur", "Saharanpur", "Jhansi", "Ayodhya"),
        "Uttarakhand" to listOf("Dehradun", "Haridwar", "Nainital", "Udham Singh Nagar", "Chamoli", "Pithoragarh", "Almora", "Pauri Garhwal", "Tehri Garhwal", "Uttarkashi", "Rudraprayag", "Bageshwar"),
        "West Bengal" to listOf("Kolkata", "North 24 Parganas", "South 24 Parganas", "Howrah", "Hooghly", "Darjeeling", "Jalpaiguri", "Paschim Medinipur", "Purba Medinipur", "Murshidabad", "Nadia", "Malda", "Kalimpong"),
        // UTs
        "Andaman and Nicobar Islands" to listOf("South Andaman (Port Blair)", "North and Middle Andaman", "Nicobar"),
        "Chandigarh" to listOf("Chandigarh"),
        "Dadra and Nagar Haveli and Daman and Diu" to listOf("Daman", "Diu", "Dadra and Nagar Haveli (Silvassa)"),
        "Delhi (NCT)" to listOf("New Delhi", "Central Delhi", "North Delhi", "South Delhi", "East Delhi", "West Delhi", "North East Delhi", "North West Delhi", "South West Delhi", "South East Delhi", "Shahdara"),
        "Jammu and Kashmir" to listOf("Srinagar", "Jammu", "Anantnag", "Baramulla", "Pulwama", "Kupwara", "Budgam", "Kathua", "Udhampur", "Rajouri", "Poonch", "Doda", "Reasi"),
        "Ladakh" to listOf("Leh", "Kargil"),
        "Lakshadweep" to listOf("Kavaratti", "Agatti", "Amini", "Andrott"),
        "Puducherry" to listOf("Puducherry", "Karaikal", "Mahe", "Yanam")
    )

    val sampleBroadcasts = listOf(
        DisasterBroadcastRecord(
            id = "ALERT-NDMA-9402",
            disasterType = "Cyclone / Storm Surge",
            emoji = "🌀",
            state = "Odisha",
            district = "Puri",
            city = "Puri Coastal Belt",
            severity = AlertSeverity.RED,
            instructions = "Severe cyclonic storm approaching coast. Immediate mass evacuation initiated within 5km coastal line. Storm shelters active.",
            authorityId = "SDMA-ODISHA-DISP01",
            timestamp = "10 mins ago"
        ),
        DisasterBroadcastRecord(
            id = "ALERT-NDMA-9398",
            disasterType = "Landslide / Mudslide",
            emoji = "⛰️",
            state = "Kerala",
            district = "Wayanad",
            city = "Meppadi & Chooralmala",
            severity = AlertSeverity.RED,
            instructions = "Triggered by continuous heavy rainfall. Rescue operations deployed. Riverbank residents move immediately to higher elevation camps.",
            authorityId = "NDRF-HQ-BN04",
            timestamp = "42 mins ago"
        ),
        DisasterBroadcastRecord(
            id = "ALERT-NDMA-9385",
            disasterType = "Cloudburst / Extreme Rain",
            emoji = "🌩️",
            state = "Uttarakhand",
            district = "Chamoli",
            city = "Joshimath Sector",
            severity = AlertSeverity.ORANGE,
            instructions = "High intensity cloudburst reported in upper catchment. Alaknanda river level rising sharply. Lowland warnings sounded.",
            authorityId = "UK-SDRF-OPERATIONS",
            timestamp = "2 hours ago"
        ),
        DisasterBroadcastRecord(
            id = "ALERT-NDMA-9371",
            disasterType = "Flood / Flash Flood",
            emoji = "🌊",
            state = "Assam",
            district = "Kamrup Metropolitan (Guwahati)",
            city = "Guwahati Lowlands",
            severity = AlertSeverity.ORANGE,
            instructions = "Brahmaputra flowing 1.2m above danger mark. Inundation alerts across Pandu and Bharalumukh sectors.",
            authorityId = "ASDMA-STATE-HQ",
            timestamp = "4 hours ago"
        )
    )
}
