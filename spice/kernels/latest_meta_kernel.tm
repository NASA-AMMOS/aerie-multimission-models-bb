 KPL/MK

           File name: latest_meta_kernel.tm

           Here are the SPICE kernels required for my application
           program.

           Note that kernels are loaded in the order listed. Thus
           we need to list the highest priority kernel last.


           \begindata

           PATH_VALUES       = ( 'spice/kernels' )

           PATH_SYMBOLS      = ( 'KERNELS' )


           KERNELS_TO_LOAD = (

              '$KERNELS/spk_ref_210111_251021_210111.bsp',
              '$KERNELS/naif0012.tls',
              '$KERNELS/pck00011.tpc'

           )

           \begintext

           End of meta-kernel
