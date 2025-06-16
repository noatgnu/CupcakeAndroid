package info.proteo.cupcake.data.remote.api

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.reagent.Reagent
import retrofit2.http.*

interface ReagentApi {
    @GET("api/reagent/")
    suspend fun getReagents(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): LimitOffsetResponse<Reagent>

    @GET("api/reagent/{id}/")
    suspend fun getReagentById(
        @Path("id") id: Int
    ): Reagent

    @POST("api/reagent/")
    suspend fun createReagent(
        @Body reagent: Map<String, String>
    ): Reagent

    @PUT("api/reagent/{id}/")
    suspend fun updateReagent(
        @Path("id") id: Int,
        @Body reagent: Map<String, String>
    ): Reagent

    @DELETE("api/reagent/{id}/")
    suspend fun deleteReagent(
        @Path("id") id: Int
    )
}
